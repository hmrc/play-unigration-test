
# Play Unigration Test

[![Download](https://api.bintray.com/packages/hmrc/releases/play-unigration-test/images/download.svg)](https://bintray.com/hmrc/releases/play-unigration-test/_latestVersion)

"Unigration testing" is a term I picked up from [Richard Beton](https://www.bigbeeconsultants.uk/). Although he makes no claims to having invented
the term, I don't know its original inventor so credit it to him here.

As can be inferred from the name, a "unigration" test sits somewhere between a unit and an integration test. The idea is that, in this style of 
testing, integration-style tests are run "off the stack" (i.e. without having to worry about assigning ports etc). It is the same principle
that informs the likes of `GuiceOneAppPerSuite` and other supporting test harnesses provided by a variety of frameworks.

In the context of a Scala/Play microservice, this approach is particularly useful as it allows for the creation of a suite of fast-running
high-level tests that cover frequently ignored code, such as routes files, application configurations and so forth. It is not anticipated
that such tests should replace unit or integration tests entirely. Rather, they should be used as an additional layer of testing that serves
to "prove" functional aspects of the application at a high-level in terms of things like exposed endpoint URLs and the requests and responses
they should deliver under given circumstances.

This library aims to provide a suite of test harnesses which support the rapid creation of tests in this style. The package structure is
divided into "assertions", "behaviours", "specs", and a few supporting utilities (in the top level `uk.gov.hmrc.test.unigration` package).

The "specs" package provides base traits for test authors to use to write tests.

The "behaviours" package provides traits that implement "when" style actions, performing any necessary customisation of the Play app in order
to support these actions.

Unsurprisingly, the "assertions" package provides traits that implement assertions that can be made about the post-condition of the "when". 

## Example Usage

At present, the library provides a minimal set of test harnesses for writing unigration tests against frontend controllers and making assertions
about HTML responses and HTTP statuses. It is hoped that, in future, additional sets of behaviours, specs, and assertions might be added to
support other common testing requirements relating to the production of HMRC Scala/Play microservices (e.g. authentication, session caching etc).

An example of testing an HTTP GET endpoint that returns some HTML:

```scala
class MyControllerSpec extends ControllerUnigrationSpec {

    "GET /some-app-url" should {
    
      "return OK" in withRequest("GET", "/some-app-url") { result =>
        wasOk(result)
      }
      
      "contain the foo form" in withRequest("GET", "/some-app-url") { result =>
        includeForm(result, routes.MyController.handleForm(), HttpVerbs.POST)
      }
    
    }

}
```

In the above example, the `withRequest` test harness will route the given request and execute the test provided in the anonymous function which
has access to the result.

An example of testing an authenticated HTTP GET endpoint:

```scala
class MyControllerSpec extends ControllerUnigrationSpec with AuthenticationBehaviours {
  
  "GET /some-app-url" should {
    
    "return OK when user is signed in" in withSignedInUser(userFixture()) { (headers, session, tags) =>
      withRequest("GET", "/some-app-url", headers = headers, session = session, tags = tags) { result =>
        wasOk(result)
      }
    }
    
    "redirect user to login page when no signed in" in withoutSignedInUser() {
      withRequest("GET", "/some-app-url") { result =>
        wasRedirected(ggLoginRedirectUri("/some-app-url"), result)
      }
    }
    
  }
  
}
```

In the above example, the `withSignedInUser` test harness sets up the auth client and associated request properties in
order to subsequently route a request that requires authentication. Alternatively, `withoutSignedInUser` sets up the
auth client to return a failed future with a `NoActiveSession` exception.

As it currently stands, the test harnesses provided by `AuthenticationBehaviours` are quite limited in how it expects
the auth client to be used and could do with refinement to make it more general purpose.

An example of testing an endpoint that depends on session cache data:

```scala
class MyControllerSpec extends ControllerUnigrationSpec with AuthenticationBehaviours with SessionCacheBehaviours {

  "GET /some-app-url" should {
  
    "display session cache data in" in in withSignedInUser(userFixture()) { (_, _, _) => 
      withSessionCache[MyJsonCaseClass]("someForm", MyJsonCaseClass("foo")) { sessionCache =>
        withRequest("GET", "/some-app-url") { result =>
          includesHtmlTag(result, "h1", "foo")
        }
      }
    }
  
  }

}
```

In the above example, the `withSessionCache` test harness sets up the given data in a mock cache implementation that has
been bound to the `SessionCache` type in the running Guice app.

# License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").