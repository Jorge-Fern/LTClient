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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
public class LTClient {


    private final String SERVER_URL;
    private final HttpClient httpClient;


    /**
     * Set the base server URL and creates a new HttpClient that will be used for requests.
     *
     * @param serverURL the base server URL be it an IP or domain. examples: "http://10.30.10.30:8010" or "https://api.yoursite.com/ltool"
     */
    public LTClient(String serverURL) {
        this(serverURL, null, null);
    }


    /**
     * Set the base server URL and creates a new HttpClient that will be used for requests.
     * If username and password are provided an authenticator will be set to do basic authentication with the server
     * (in case server is behind reverse proxy that requires auth).
     *
     * @param serverURL the base server URL be it an IP or domain. examples: "http://10.30.10.30:8010" or "https://api.yoursite.com/ltool"
     * @param username  if the server requires authentication (if password is not specified is ignored)
     * @param password  if the server requires authentication (if username is not specified is ignored)
     */
    public LTClient(String serverURL, String username, String password) {

        SERVER_URL = serverURL;

        HttpClient.Builder builder = HttpClient.newBuilder();

        if (username != null && password != null) {
            builder = builder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            username,
                            password.toCharArray());
                }
            });
        }


        httpClient = builder.build();
    }


    /**
     * Make request to configured server to check the provided text.
     * Parameter that are not to be included should be passed as null.
     * <p>
     * the docs are a copy of <a href="https://languagetool.org/http-api/swagger-ui/#!/default/post_check">Official Docs</a>
     *
     * @param language          A language code like en-US, de-DE, fr, or auto to guess the language automatically (see preferredVariants below). For languages with variants (English, German, Portuguese) spell checking will only be activated when you specify the variant, e.g. en-GB instead of just en.
     * @param text              The text to be checked. This or 'data' is required.
     * @param data              The text to be checked, given as a JSON document that specifies what's text and what's markup. This or 'text' is required. Markup will be ignored when looking for errors. Example text:
     *                          A <b>test</b>
     *                          JSON for the example text:
     *                          {"annotation":[
     *                          {"text": "A "},
     *                          {"markup": "<b>"},
     *                          {"text": "test"},
     *                          {"markup": "</b>"}
     *                          ]}
     *                          If you have markup that should be interpreted as whitespace, like <p> in HTML, you can have it interpreted like this:
     *                          {"markup": "<p>", "interpretAs": "\n\n"}
     *                          The 'data' feature is not limited to HTML or XML, it can be used for any kind of markup. Entities will need to be expanded in this input.
     * @param username          Set to get Premium API access: Your username/email as used to log in at languagetool.org.
     * @param apiKey            Set to get Premium API access: your API key
     * @param dicts             Comma-separated list of dictionaries to include words from; uses special default dictionary if this is unset
     * @param motherTongue      A language code of the user's native language, enabling false friends checks for some language pairs.
     * @param preferedVariants  Comma-separated list of preferred language variants. The language detector used with language=auto can detect e.g. English, but it cannot decide whether British English or American English is used. Thus this parameter can be used to specify the preferred variants like en-GB and de-AT. Only available with language=auto. You should set variants for at least German and English, as otherwise the spell checking will not work for those, as no spelling dictionary can be selected for just en or de.
     * @param enabledRules      IDs of rules to be enabled, comma-separated. Note that 'level' still applies, so the rule won't run unless 'level' is set to a level that activates the rule.
     * @param disabledRules     IDs of rules to be disabled, comma-separated
     * @param enabledCategories IDs of categories to be enabled, comma-separated
     * @param disabledCategries IDs of categories to be disabled, comma-separated
     * @param enabledOnly       If true, only the rules and categories whose IDs are specified with enabledRules or enabledCategories are enabled.
     * @param level             (default;picky)If set to picky, additional rules will be activated, i.e. rules that you might only find useful when checking formal text.
     * @return LTRresponse object representing the response info.
     */
    public LTResponse check(String language, String text, String data, String username, String apiKey, String dicts, String motherTongue, String preferedVariants, String enabledRules, String disabledRules, String enabledCategories, String disabledCategries, Boolean enabledOnly, String level) throws IOException, InterruptedException {


        //convert params to urlencoded string
        String formData = getFormData(language, text, data, username, apiKey, dicts, motherTongue, preferedVariants, enabledRules, disabledRules, enabledCategories, disabledCategries, enabledOnly, level);


        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/v2/check"))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        var response = httpClient.send(request, new JsonBodyHandler<>(LTResponse.class));

        if (response.statusCode() == HttpURLConnection.HTTP_OK) {
            return response.body().get();


        } else {
            log.error("error:" + response.statusCode());
            throw new ConnectException("error code: "+response.statusCode());

        }

    }


    /**
     * Does the same as {@link #check(String, String, String, String, String, String, String, String, String, String, String, String, Boolean, String) check}, but in Async mode. (returns a {@link CompletableFuture})
     * The possible exceptions must be handled when getting the result;
     *
     * @param language          A language code like en-US, de-DE, fr, or auto to guess the language automatically (see preferredVariants below). For languages with variants (English, German, Portuguese) spell checking will only be activated when you specify the variant, e.g. en-GB instead of just en.
     * @param text              The text to be checked. This or 'data' is required.
     * @param data              The text to be checked, given as a JSON document that specifies what's text and what's markup. This or 'text' is required. Markup will be ignored when looking for errors. Example text:
     *                          A <b>test</b>
     *                          JSON for the example text:
     *                          {"annotation":[
     *                          {"text": "A "},
     *                          {"markup": "<b>"},
     *                          {"text": "test"},
     *                          {"markup": "</b>"}
     *                          ]}
     *                          If you have markup that should be interpreted as whitespace, like <p> in HTML, you can have it interpreted like this:
     *                          {"markup": "<p>", "interpretAs": "\n\n"}
     *                          The 'data' feature is not limited to HTML or XML, it can be used for any kind of markup. Entities will need to be expanded in this input.
     * @param username          Set to get Premium API access: Your username/email as used to log in at languagetool.org.
     * @param apiKey            Set to get Premium API access: your API key
     * @param dicts             Comma-separated list of dictionaries to include words from; uses special default dictionary if this is unset
     * @param motherTongue      A language code of the user's native language, enabling false friends checks for some language pairs.
     * @param preferedVariants  Comma-separated list of preferred language variants. The language detector used with language=auto can detect e.g. English, but it cannot decide whether British English or American English is used. Thus this parameter can be used to specify the preferred variants like en-GB and de-AT. Only available with language=auto. You should set variants for at least German and English, as otherwise the spell checking will not work for those, as no spelling dictionary can be selected for just en or de.
     * @param enabledRules      IDs of rules to be enabled, comma-separated. Note that 'level' still applies, so the rule won't run unless 'level' is set to a level that activates the rule.
     * @param disabledRules     IDs of rules to be disabled, comma-separated
     * @param enabledCategories IDs of categories to be enabled, comma-separated
     * @param disabledCategries IDs of categories to be disabled, comma-separated
     * @param enabledOnly       If true, only the rules and categories whose IDs are specified with enabledRules or enabledCategories are enabled.
     * @param level             (default;picky)If set to picky, additional rules will be activated, i.e. rules that you might only find useful when checking formal text.
     * @return CompletableFuture
     */
    public CompletableFuture<HttpResponse<Supplier<LTResponse>>> checkAsync(String language, String text, String data, String username, String apiKey, String dicts, String motherTongue, String preferedVariants, String enabledRules, String disabledRules, String enabledCategories, String disabledCategries, Boolean enabledOnly, String level) {


        //convert params to urlencoded string
        String formData = getFormData(language, text, data, username, apiKey, dicts, motherTongue, preferedVariants, enabledRules, disabledRules, enabledCategories, disabledCategries, enabledOnly, level);


        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(SERVER_URL + "/v2/check"))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();


        return httpClient.sendAsync(request, new JsonBodyHandler<>(LTResponse.class));

    }


    /**
     * Make request to configured server to check the provided text.
     * All other parameters will be set to default values or omitted
     *
     * @param language A language code like en-US, de-DE, fr, or auto to guess the language automatically (see preferredVariants below). For languages with variants (English, German, Portuguese) spell checking will only be activated when you specify the variant, e.g. en-GB instead of just en.
     * @param text     The text to be checked.
     * @return Object representing the response from server
     */
    public LTResponse check(@NonNull String language, @NonNull String text) throws IOException, InterruptedException {
        return this.check(language,
                text,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

    }


    /**
     * Make request to configured server to check the provided text.
     * All other parameters will be set to default values or omitted
     *
     * @param language A language code like en-US, de-DE, fr, or auto to guess the language automatically (see preferredVariants below). For languages with variants (English, German, Portuguese) spell checking will only be activated when you specify the variant, e.g. en-GB instead of just en.
     * @param text     The text to be checked.
     * @return Object representing the response from server
     */
    public CompletableFuture<HttpResponse<Supplier<LTResponse>>> checkAsync(@NonNull String language, @NonNull String text) {
        return this.checkAsync(language,
                text,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

    }


    /**
     * Parameters must sent in x-www-form-urlencoded, this function encodes all non NULL params
     *
     * @return String with the encoded params
     */
    private String getFormData(String language, String text, String data, String username, String apiKey, String dicts, String motherTongue, String preferedVariants, String enabledRules, String disabledRules, String enabledCategories, String disabledCategries, Boolean enabledOnly, String level) {
        String formData = URLEncoder.encode("language", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(language, StandardCharsets.UTF_8);

        if (text != null)
            formData += "&" + URLEncoder.encode("text", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        if (data != null)
            formData += "&" + URLEncoder.encode("data", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(data, StandardCharsets.UTF_8);

        if (username != null)
            formData += "&" + URLEncoder.encode("username", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(username, StandardCharsets.UTF_8);

        if (apiKey != null)
            formData += "&" + URLEncoder.encode("apiKey", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        if (dicts != null)
            formData += "&" + URLEncoder.encode("dicts", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(dicts, StandardCharsets.UTF_8);

        if (motherTongue != null)
            formData += "&" + URLEncoder.encode("motherTongue", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(motherTongue, StandardCharsets.UTF_8);

        if (apiKey != null)
            formData += "&" + URLEncoder.encode("apiKey", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        if (preferedVariants != null)
            formData += "&" + URLEncoder.encode("preferedVariants", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(preferedVariants, StandardCharsets.UTF_8);

        if (enabledRules != null)
            formData += "&" + URLEncoder.encode("enabledRules", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(enabledRules, StandardCharsets.UTF_8);


        if (disabledRules != null)
            formData += "&" + URLEncoder.encode("disabledRules", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(disabledRules, StandardCharsets.UTF_8);

        if (enabledCategories != null)
            formData += "&" + URLEncoder.encode("enabledCategories", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(enabledCategories, StandardCharsets.UTF_8);


        if (disabledCategries != null)
            formData += "&" + URLEncoder.encode("disabledCategries", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(disabledCategries, StandardCharsets.UTF_8);


        if (enabledOnly != null)
            formData += "&" + URLEncoder.encode("enabledOnly", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(enabledOnly.toString(), StandardCharsets.UTF_8);


        if (level != null)
            formData += "&" + URLEncoder.encode("level", StandardCharsets.UTF_8) + "=" + URLEncoder.encode(level, StandardCharsets.UTF_8);
        return formData;
    }


}


