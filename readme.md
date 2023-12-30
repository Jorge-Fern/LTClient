# LTCLient - A Java Http client for LanguageTool

LTCLient is simple http client app. Its aim is to provide a simple and fast way to interact with LanguageToolServer API.
This library is under active development and doesn't yet support all functions provides by the LanguageTool
API (https://languagetool.org/http-api/swagger-ui/#/default)

The existing official languagetool-http-client [link](https://github.com/languagetool-org/languagetool) is too complex
and uses lots of external dependencies, wich causes conflicts with modern Java(11+) JPMS applications.
In my use case even using [ModiTect](https://github.com/moditect/moditect) to "fix" the missing module.info was not
able to use JLink/JPackage.

This library aims to be lightweight and use as litle external dependencies as possible.


## How to use

First intialize the library by creating a new instance and providing server URL

````java

LTClient ltClient = new LTClient("http://10.30.10.30:8010", null, null);

````

then you can use the check() and checkAsync() methods

````java

LTResponse response = ltClient.check("pt-PT", "Texto para verificar se existe algum erro");

response.getMatches().forEach(ltMatch -> {

        System.out.println("Error between chars: " + ltMatch.getOffset() + " - " + (ltMatch.getOffset() + ltMatch.getLength()));

        });



//or async method

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

//if you must make sure the request has ended before continuing call join()
voidCompletableFuture.join();


````