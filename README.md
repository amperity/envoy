Environment Sentry
==================

A Clojure library compatible with [environ](https://github.com/weavejester/environ)
which adds tracking of which environment variables are referenced, whitelisting,
descriptions, and more!


## Usage

A quick overview of sentry usage:

```clojure
=> (require '[environ.sentry :as env :refer [defenv env]])

=> (defenv :http-port
     "TCP port to run the HTTP server on."
     :type :integer)

; Look up the declared variable, get an int back:
=> (env :http-port)
8080

; Sentry records variable access:
=> @env/accesses
{:http-port 1}

; Try an undeclared environment variable:
=> (env :user)
; WARNING: Access to undeclared env variable :user
"pjfry"

; The warning was logged because of the behavior setting:
=> (env/behavior :undeclared-access)
:warn

; We can also set it to be stricter:
=> (env/set-behavior! :undeclared-access :abort)

=> (:foo env)
; ExceptionInfo Access to undeclared env variable :foo  clojure.core/ex-info

; Overrides have a behavior setting too:
=> (env/behavior :undeclared-override)
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
```


## Configuration

### Behaviors

Sentry supports a few behavior settings which control what happens in various
situations. All behavior options support the following values:

| Behavior     | Description |
| ------------ | ----------- |
| `nil`        | No behavior. |
| `:warn`      | Log a warning. |
| `:abort`     | Throw an exception. |

There are two global behaviors:

- `:undeclared-access` triggers when a variable is looked up in the environment
  map which has not been defined.
- `:undeclared-override` triggers when an undefined variable is associated into
  the environment map.

### Variable Definitions

Variables can be declared using the `defenv` macro or its helper function
`declare-env-var!`. Definitions can have the following attributes:

| Attribute      | Definition |
| -------------- | ---------- |
| `:ns`          | Namespace the variable was defined in. |
| `:line`        | Line number of the definition in the source file. |
| `:description` | Human-readable description of the variable. |
| `:type`        | Value type to parse the variable as. See below. |
| `:missing`     | Behavior if the variable is accessed with no configured value. |

The first three are added automatically as part of the macro.

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


## TODO

- validate that environ files from `.lein-env` don't contain undocumented vars
- -main function that can run the lint task above (needs to load all the code though?)
- report defined variables which are never accessed?


## License

Licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) file
for rights and restrictions.
