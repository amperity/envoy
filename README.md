Envoy
=====

[![CircleCI](https://circleci.com/gh/amperity/envoy.svg?style=svg&circle-token=43f6c88e6aca5140c0da783c55138f281ef696fb)](https://circleci.com/gh/amperity/envoy)
[![envoy cljdoc](https://cljdoc.xyz/badge/amperity/envoy)](https://cljdoc.xyz/d/amperity/envoy/CURRENT)

A Clojure library compatible with [environ](https://github.com/weavejester/environ)
which adds tracking of which environment variables are referenced, whitelisting,
descriptions, and more!

## Installation

Library releases are published on Clojars. To use the latest version with
Leiningen, add the following to your project dependencies:

[![Clojars Project](http://clojars.org/amperity/envoy/latest-version.svg)](http://clojars.org/amperity/envoy)

## Usage

A quick overview of envoy usage:

```clojure
=> (require '[envoy.core :as env :refer [defenv env]])

=> (defenv :http-port
     "TCP port to run the HTTP server on."
     :type :integer)

; Look up the declared variable, get an int back:
=> (env :http-port)
8080

; Envoy records variable access:
=> @env/accesses
{:http-port 1}

; Try an undeclared environment variable:
=> (env :user)
; WARNING: Access to undeclared env variable :user
"pjfry"

; The warning was logged because of the behavior setting:
=> (envoy.check/behaviors :undeclared-access)
:warn

; We can also set it to be stricter:
=> (envoy.check/set-behavior! :undeclared-access :abort)

=> (:foo env)
; ExceptionInfo Access to undeclared env variable :foo  clojure.core/ex-info

; Overrides have a behavior setting too:
=> (envoy.check/behaviors :undeclared-override)
:warn

=> (assoc env :foo "bar")
; WARNING: Overriding undeclared env variable :foo
{:foo "bar", :http-port ...}

; Variables can have :missing behavior for situations where the var must not
; resolve to nil:
=> (defenv :secret-key
     "Essential for accessing the data!"
     :missing :warn)

; Calling without a default value triggers:
=> (env :secret-key)
; WARNING: Access to env variable :secret-key which has no value
nil

; Providing a default is okay though:
=> (env :secret-key "53CR37")
"53CR37"

; Still watching all those accesses!
=> @env/accesses
{:http-port 1
 :user 1
 :foo 1
 :secret-key 2}

; Need to change a value from the REPL, but don't want to restart?
=> (env/set-env! :http-port "8085")
=> (env :http-port)
8085
```

### Linting

If you're using `lein-env` or `boot-env` to pass environment configuration from
your build tool to the process, you can use the `-main` function in
`envoy.tools` to check that you're not providing values for any undeclared
variables. To run the lint task, use:

```
lein run -m envoy.tools lint [namespace ...]
```

Any namespaces provided will be loaded before the check runs, in case you need
to pull in variable definitions. Typically, you should provide the top-level
namespace with the `-main` entry point into the code.

### Reporting

Similarly, you can use the tools namespace to print out a report of the variable
definitions known to envoy.

```
lein run -m envoy.tools report [namespace ...]
```

This will give you a table like the following:

```
| Name                 | Type    | Declaration             | Description                         |
|--------------------- | ------- | ------------------------| ------------------------------------|
| :secret-key          | string  | example.data.crypto:187 | Essential for accessing the data!   |
| :http-port           | integer | example.http.server:22  | TCP port to run the HTTP server on. |
```

The table is in Github-compatible Markdown to make it easy to paste into
documentation.

## Configuration

### Variable Definitions

Variables can be declared using the `defenv` macro or its helper function
`declare-env-var!`. Definitions can have the following attributes:

| Attribute      | Definition |
| -------------- | ---------- |
| `:ns`          | Namespace the variable was defined in. |
| `:line`        | Line number of the definition in the source file. |
| `:description` | Human-readable description of the variable. |
| `:type`        | Value type to parse the variable as. See below. |
| `:default`     | Default value, cannot be set with `:missing` and must match `:type`. |
| `:missing`     | Behavior if the variable is accessed with no configured value. |

The first three are added automatically as part of the `defenv` macro.

### Value Types

By default, all variables are treated like `:string` types, which directly reads
the value from the environment. Other types apply parsing functions to the
values:

| Type       | Definition |
| ---------- | ---------- |
| `:string`  | Normal string variable. |
| `:keyword` | Parse the value as a keyword (without the leading colon). |
| `:boolean` | Converts 'bool-esque' values such as `""` `"0"`, `"f"`, `"no"`, and so on to `false` and everything else to `true`. |
| `:integer` | Parse the value as an integer. |
| `:decimal` | Parse the value as a floating point number. |
| `:list`    | Treats the value as a comma-separated list of strings. |

### Behaviors

Envoy supports behavior settings which control what happens in various
situations. There are a few different behaviors:

| Behavior                | Type     | Trigger     |
| ----------------------- | -------- | ----------- |
| **undeclared-access**   | global   | An undeclared variable is looked up in the environment map. |
| **undeclared-override** | global   | An undeclared variable is associated into the environment map. |
| **undeclared-config**   | global   | An environment file provides a value for an undeclared variable. |
| **missing-access**      | variable | A variable is accessed without a default and is not present in the environment. |

All behavior options support the following values:

| Setting      | Description |
| ------------ | ----------- |
| `nil`        | No behavior. |
| `:warn`      | Log a warning. |
| `:abort`     | Throw an exception. |

## License

Licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file
for rights and restrictions.
