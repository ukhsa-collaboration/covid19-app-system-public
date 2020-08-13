#!/usr/bin/env python3
import os
import re
import argparse
import glob


def slurp_file(path):
    data = ''
    try:
        with open(path, 'r') as fin:
            data = fin.read()
    except IOError:
        pass
    if not (data and not data.isspace()):
        assent = input('Warning: ' + path + ' is empty; do you want to continue? [yN]')
        if not assent.lower().startswith('y'):
            exit(1)
    return data


def process_file(std_header, std_footer, path):
    print("processing", path)
    bak_path = path + '.bak'
    os.rename(path, bak_path)
    with open(bak_path, 'r') as fin, open(path, 'w') as fout:
        data = fin.read()
        data = re.sub(r'(//\s*BEGIN STANDARD HEADER).*?(//\s*END STANDARD HEADER)',
                      r'\1\n' +
                      std_header +
                      r'\2', data, flags=re.IGNORECASE+re.MULTILINE+re.DOTALL)
        data = re.sub(r'(//\s*BEGIN STANDARD FOOTER).*?(//\s*END STANDARD FOOTER)',
                      r'\1\n' +
                      std_footer +
                      r'\2', data, flags=re.IGNORECASE+re.MULTILINE+re.DOTALL)
        fout.write(data)
    pass


def process_folder(std_header, std_footer, pattern, wd):
    wd_realpath = os.path.realpath(wd)
    pattern = os.path.join(wd_realpath, os.path.join('**', pattern))
    for path in glob.glob(pattern, recursive=True):
        process_file(std_header, std_footer, path)
    pass


parser = argparse.ArgumentParser(description='Replace standard header/footer sections in AsciiDoc files')
parser.add_argument('-H', '--header', default='std_header.adoc',
                    help='Path to contents of new standard header, default=std_header.adoc')
parser.add_argument('-F', '--footer', default='std_footer.adoc',
                    help='Path to contents of new standard footer, default=std_footer.adoc')
parser.add_argument('-P', '--pattern', default='*.adoc', help='Filename glob pattern, , default=*.adoc')
parser.add_argument('workdirs', nargs='*', default=[os.getcwd()],
                    help='Path(s) to folder(s) containing AsciiDoc files, default=.')
args = parser.parse_args()
std_header = slurp_file(args.header)
std_footer = slurp_file(args.footer)
for wd in args.workdirs:
    process_folder(std_header, std_footer, args.pattern, wd)
