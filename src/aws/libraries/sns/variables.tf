variable "name" {
  description = "Name of SNS topic"
  type        = string
}

variable "policy_statements" {
  description = "Additional statements to add to the default SNS topics inline policy."
  type = list(object({
    sid     = string
    actions = list(string),
    effect  = string,
    principals = object({
      type        = string,
      identifiers = list(string)
    }),
    conditions = list(object({
      test     = string,
      variable = string,
      values   = list(string)
    }))
  }))
  default = []
}

variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}
