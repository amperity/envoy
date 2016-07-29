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

## TODO

- validate that environ files from `.lein-env` don't contain undocumented vars
- -main function that can run the lint task above (needs to load all the code though?)
- report defined variables which are never accessed?
