variable "tags" {
  description = "A map of key-value labels used to tag AWS resources"
  type        = map(string)
}

variable "message_delivery_delay" {
  description = "How long messages are delayed before delivered to subscribers"
}

variable "name" {
  description = "The name you want to use to identify this message queue."
}
variable "enable_dead_letter_queue" {
  description = "Enables a dead letter queue."
  default     = false
  type        = bool
}
variable "dead_letter_queue_alarm_topic_arn" {
  description = "Arn of alarm topic for not deliverable messages."
  default     = null
}
