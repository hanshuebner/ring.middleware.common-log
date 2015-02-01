# ring.middleware.common-log #

[![Build Status](https://travis-ci.org/hanshuebner/ring.middleware.common-log.svg?branch=master)](https://travis-ci.org/hanshuebner/ring.middleware.common-log)

A ring middleware to log requests in
[Common Log](http://en.wikipedia.org/wiki/Common_Log_Format) format.
Supports automatic renaming of the log file if a size limit is
exceeded.

## Installation ##

Latest stable version:

[![Clojars Project](http://clojars.org/ring.middleware.common-log/latest-version.svg)](http://clojars.org/ring.middleware.common-log)

Add this to your dependencies in `project.clj`.

## Features ##

 - Writes log entries in [Common Log](http://en.wikipedia.org/wiki/Common_Log_Format) which can be processed by common web log processing tools
 - Buffers writes to log file internally to reduce overhead
 - Can automatically rename log file when a set size limit is reached
 - Logs to standard output if no log filename has been set

## Summary ##

To log requests to the file `/tmp/access.log` with a size limit of one
megabyte, use this:

```clojure
(ns my.app
  (:require [ring.middleware.common-log :refer [wrap-with-common-log]))

(def app
  (-> handler
      (wrap-with-common-log :filename "/tmp/access.log"
                            :max-file-size 1000000)))
```

## Size limit ##

When the `:max-file-size` option is used, the log writer checks
whether the log file is larger than that given size, in bytes.  If so,
it renames the current file name to include a time stamp before the
file extension and opens a new log file with the name defined with the
`:filename` option.  The time stamp is in `YYYYMMDDHHMMSS` format, so
for example if the `:filename` option was set to `/tmp/access.log`, a
possible archival file name would be `/tmp/access-201502011319.log`.

## License ##

Copyright © 2015 Hans Hübner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
