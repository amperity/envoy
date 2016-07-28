env-tracker
===========

A Clojure library compatible with [environ](https://github.com/weavejester/environ)
which adds tracking of which environment variables are referenced, whitelisting,
descriptions, and more!

## TODO

- validate that environ files from `.lein-env` don't contain undocumented vars
- 'env' var that acts like a map
- function to provide stats on accesses
