# ring.middleware.common-log

A ring middleware to log requests in Common Log format.  Supports
automatic renaming of the log file if a size limit is exceeded.

## Summary

To log requests to the file `/tmp/access.log` with a size limit of
1MB, use this:

```clojure
(ns my.app
  (:require [ring.middleware.common-log :refer [wrap-with-common-log]))

(def app
  (-> handler
      (wrap-with-common-log :filename "/tmp/access.log"
                            :rotate? true
                            :max-file-size 1000000)))
```

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
