package uk.nhs.nhsx.core

import com.amazonaws.services.lambda.runtime.Context

fun interface Handler<T, R> : (T, Context) -> R
