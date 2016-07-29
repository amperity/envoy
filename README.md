env-tracker
===========

A Clojure library compatible with [environ](https://github.com/weavejester/environ)
which adds tracking of which environment variables are referenced, whitelisting,
descriptions, and more!

## Usage

```clojure
(require '[environ.sentry :as env :refer [defenv env]])

(defenv :http-port
  "TCP port to run the HTTP server on."
  :missing :warn
  :type :integer)



(env/set-behavior! :undeclared-access :abort)

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

### Variable Definitions

| Attribute      | Definition |
| -------------- | ---------- |
| `:ns`          | Namespace the variable was defined in. |
| `:line`        | Line number of the definition in the source file. |
| `:description` | Human-readable description of the variable. |
| `:type`        | Value type to parse the variable as. See below. |
| `:missing`     | Behavior if the variable is accessed with no configured value. |

## TODO

- validate that environ files from `.lein-env` don't contain undocumented vars
- -main function that can run the lint task above (needs to load all the code though?)
- :default value support (NOPE)
- :required support? throw on access to var with no configured value and no default value
- report defined variables which are never accessed?
