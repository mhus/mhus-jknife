/*

    Copyright (C) 2002 Mike Hummel (mh@mhus.de)

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

            http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

*/
package de.mhus.jknife.jllm;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runs a streaming chat request, measures ttft (time to first token) and total latency and collects the full response
 * text.
 */
final class StreamingRunner implements StreamingChatResponseHandler {

    record Result(String text, ChatResponse response, long totalNanos, long ttftNanos, Throwable error) {
    }

    private final long startNanos = System.nanoTime();
    private final CountDownLatch latch = new CountDownLatch(1);
    private final StringBuilder text = new StringBuilder();
    private volatile long ttftNanos = -1;
    private volatile ChatResponse response;
    private volatile Throwable error;

    @Override
    public void onPartialResponse(String partial) {
        if (ttftNanos < 0)
            ttftNanos = System.nanoTime() - startNanos;
        synchronized (text) {
            text.append(partial);
        }
    }

    @Override
    public void onCompleteResponse(ChatResponse response) {
        this.response = response;
        latch.countDown();
    }

    @Override
    public void onError(Throwable error) {
        this.error = error;
        latch.countDown();
    }

    /**
     * Waits for the streaming to complete (streaming callbacks may arrive on other threads, but the call also works if
     * the chat() call is blocking).
     */
    Result await(long timeoutSeconds) throws InterruptedException {
        latch.await(Math.max(1, timeoutSeconds) + 5, TimeUnit.SECONDS);
        long totalNanos = System.nanoTime() - startNanos;
        String text;
        synchronized (this.text) {
            text = this.text.toString();
        }
        var aiMessage = response == null ? null : response.aiMessage();
        if (text.isBlank() && aiMessage != null)
            text = aiMessage.text();
        return new Result(text, response, totalNanos, ttftNanos, error);
    }
}
