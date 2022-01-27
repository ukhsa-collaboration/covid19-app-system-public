import argparse
import json
from collections import defaultdict
from typing import Callable, List, Dict, Any

import arrow
import boto3
import marshmallow_dataclass
from arrow import Arrow
from mypy_boto3_logs import CloudWatchLogsClient
from mypy_boto3_logs.type_defs import StartQueryResponseTypeDef, GetQueryResultsResponseTypeDef
from waiting import wait  # type: ignore


class LogClient:
    def __init__(self, client: CloudWatchLogsClient, offset: int, sleep_seconds: int, timeout_seconds: int) -> None:
        self.client = client
        self.now = arrow.utcnow()
        self.start_time = self.now.shift(days=offset).timestamp()
        self.end_time = self.now.timestamp()
        self.sleep_seconds = sleep_seconds
        self.timeout_seconds = timeout_seconds

    def query(self, log_group_name: str, query: str) -> List[Dict[str, str]]:
        resp: StartQueryResponseTypeDef = self.client.start_query(
            logGroupName=log_group_name,
            startTime=int(self.start_time),
            endTime=int(self.end_time),
            queryString=query
        )

        results = self.__wait(resp['queryId'])

        events: List[Dict[str, str]] = []
        for row in results['results']:
            entries: Dict[str, str] = {}
            for entry in row:
                entries[entry['field']] = entry['value']
            events.append(entries)
        return events

    def __wait(self, query_id: str) -> GetQueryResultsResponseTypeDef:
        wait(self.__is_complete(query_id), sleep_seconds=self.sleep_seconds, timeout_seconds=self.timeout_seconds)
        return self.__results(query_id)

    def __is_complete(self, query_id: str) -> Callable[[], bool]:
        return lambda: self.client.get_query_results(queryId=query_id)['status'] == 'Complete'

    def __results(self, query_id: str) -> GetQueryResultsResponseTypeDef:
        return self.client.get_query_results(queryId=query_id)


@marshmallow_dataclass.dataclass
class EventEnvelope:
    metadata: Dict[str, str]
    event: Dict[str, Any]

    @property
    def aws_request_id(self) -> str:
        return self.metadata['awsRequestId']

    @property
    def timestamp(self) -> Arrow:
        return arrow.get(self.metadata['timestamp'])

    @property
    def name(self) -> str:
        return self.metadata['name']

    @property
    def cta_token(self):
        return self.event['ctaToken']

    @cta_token.setter
    def cta_token(self, v: str) -> None:
        self.event['ctaToken'] = v


class ConsoleReporter:

    @staticmethod
    def log(events: List[EventEnvelope]) -> None:
        grouped_events = defaultdict(list)
        for e in events:
            grouped_events[e.cta_token].append(e)

        for key, value in grouped_events.items():
            sorted_events = sorted(value, reverse=False, key=lambda e: e.timestamp)
            print(f"""# {key}""")
            for event in sorted_events:
                print(f"""{event.timestamp}: {event.name}""")
                print(f"""{json.dumps(event.event, indent=4, sort_keys=True)}""")
                print("")


class Logs:

    def __init__(self, client: LogClient, environment: str) -> None:
        self.client = client
        self.env = environment.lower()

    def history(self, cta_tokens: List[str]) -> List[EventEnvelope]:
        return self.cta_token_exchange(cta_tokens) + self.virology_upload(cta_tokens)

    def cta_token_exchange(self, cta_tokens: List[str]) -> List[EventEnvelope]:
        resp = self.client.query(
            log_group_name=f"/aws/lambda/te-{self.env}-virology-sub",
            query=f"""
            fields @timestamp, @message 
            | filter event.ctaToken in [{Logs.__string_list(cta_tokens)}]
            | order by event.ctaToken, @timestamp
            """
        )

        return self.__parse_message(resp)

    def virology_upload(self, cta_tokens: List[str]):
        def token_creation(tokens: List[str]) -> List[EventEnvelope]:
            resp = self.client.query(
                log_group_name=f"/aws/lambda/te-{self.env}-virology-upload",
                query=f"""
                fields @timestamp, @message
                | filter metadata.name = 'InfoEvent' and event.message like /Token gen created ctaToken/
                | parse @message /ctaToken: (?<token>[a-z0-9]+)/
                | filter token in [{Logs.__string_list(tokens)}]
                """
            )

            events: List[EventEnvelope] = []
            for row in resp:
                event = Logs.__parse_event(row['@message'])
                event.cta_token = row['token']
                events.append(event)
            return events

        def upload(events: List[EventEnvelope]) -> List[EventEnvelope]:
            lookup = {e.aws_request_id: e for e in events}
            aws_request_ids = list(lookup.keys())

            resp = self.client.query(
                log_group_name=f"/aws/lambda/te-{self.env}-virology-upload",
                query=f"""
                fields @timestamp, @message
                | filter metadata.name = "CtaTokenGen"
                | filter metadata.awsRequestId in [{Logs.__string_list(aws_request_ids)}]
                """
            )

            virology_events = Logs.__parse_message(resp)
            for event in virology_events:
                # find the original event in the lookup table and copy the CTA token
                event.cta_token = lookup[event.aws_request_id].cta_token
            return virology_events

        return upload(token_creation(cta_tokens))

    @staticmethod
    def __string_list(tokens: List[str]) -> str:
        return ','.join(list(map(lambda t: f"\"{t}\"", tokens)))

    @staticmethod
    def __parse_message(results: List[Dict[str, str]]) -> List[EventEnvelope]:
        return list(map(lambda r: Logs.__parse_event(r['@message']), results))

    @staticmethod
    def __parse_event(row: Any) -> EventEnvelope:
        schema = marshmallow_dataclass.class_schema(EventEnvelope)()
        if isinstance(row, str):
            return schema.load(json.loads(row))
        elif isinstance(row, dict):
            return schema.load(row)
        else:
            raise ValueError("failed to parse event")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Find logs associated with CTA token(s)')
    parser.add_argument('tokens', metavar='T', type=str, nargs='+', help='a CTA token')
    parser.add_argument('--offset', type=int, default=-30, choices=range(-90, 0, 10),
                        help='search for logs up to N days in the past')
    parser.add_argument('--sleep_seconds', type=int, default=10, help='interval between polling for CloudWatch results')
    parser.add_argument('--timeout_seconds', type=int, default=10 * 60, help='timeout waiting for CloudWatch results')
    parser.add_argument('--env', type=str, default='ci', help='the target environment')
    args = parser.parse_args()

    log_client = LogClient(boto3.client('logs'),
                           offset=args.offset,
                           timeout_seconds=args.timeout_seconds,
                           sleep_seconds=args.sleep_seconds)

    print(f"""Searching for CTA tokens: {args.tokens}""")

    history = Logs(log_client, environment=args.env).history(args.tokens)

    ConsoleReporter.log(history)
