env-tracker
===========

A Clojure library compatible with [environ](https://github.com/???/environ)
which adds tracking of which environment variables are referenced, whitelisting,
descriptions, and more!

## TODO

- functions for setting sentry behavior
- ideally, provide types and auto-parse (integer, decimal, list, etc)
- validate that environ files from `.lein-env` don't contain undocumented vars
- 'env' var that acts like a map
- record accesses by var
- record overrides (assoc env :foo "bar")
- function to provide stats on accesses
- modes to warn or abort on access to un-defined vars
