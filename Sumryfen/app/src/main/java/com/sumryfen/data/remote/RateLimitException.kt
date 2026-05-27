package com.sumryfen.data.remote

class RateLimitException(val retryAfterSeconds: Int) :
    Exception("Rate limit, coba lagi dalam $retryAfterSeconds detik")
