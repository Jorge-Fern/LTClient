package com.jfern.ltclient;

/*-
 * #%L
 * LTClient
 * %%
 * Copyright (C) 2023 Jorge Fernando Gonçalves
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.jfern.ltclient.POJO.LTResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
class LTClientTest {

    private static LTClient ltClient;

    @Test
    void checkAsync() {

        //make request
        CompletableFuture<Void> voidCompletableFuture = ltClient.checkAsync("pt-PT", "Texto exemplo com errorrrrrr")
                //handle exceptions
                .exceptionally(throwable -> {
                    log.error("Error connecting to server", throwable);
                    return null;
                })
                //get info from response body
                .thenApply(HttpResponse::body)
                //do something when request finishes
                .thenAccept(ltResponseSupplier -> {

                    ltResponseSupplier.get().getMatches().forEach(ltMatch -> {

                        System.out.println("Error between chars: " + ltMatch.getOffset() + " - " + (ltMatch.getOffset() + ltMatch.getLength()));

                    });

                });

        //if you must make shur the request has ended before continuing
        voidCompletableFuture.join();


    }

    @Test
    void check() {
        LTResponse response = ltClient.check("pt-PT", "TExto");

        assertNotNull(response);
        System.out.println(response.getSoftware().getName() + " - " + response.getSoftware().getVersion());
    }

    @BeforeAll
    static void setUp() {

        //public languagetool server
        ltClient = new LTClient("https://api.languagetool.org", null, null);


    }


}
