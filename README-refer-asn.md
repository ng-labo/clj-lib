### about

This is one of demo application for clj-lib.

### explanation

using clj-lib.gobgpapi, this tries to retrive all ASN from as-path each prefixes.
IP2Location(TM)LITE help me to refer country code.


IP2LOCATION-LITE-DB1.CSV is prepared in advance.

gobgpd is run with available bgp table.

example commands (insert comments manually)
```
clj-lib.core=>; load namespaces

clj-lib.core=> (use 'clj-lib.gobgpapi.core)
nil
clj-lib.core=> (use 'refer-asn.core)
nil
clj-lib.core=>; create a gobgp client

clj-lib.core=> (def gcli (gobgp-client "localhost:50051"))
#'clj-lib.core/gcli
clj-lib.core=>; retrive all prefixs and get ASN from AsPath on each prefixes 

clj-lib.core=> (def all-prefixs (get-all-prefixs gcli))
#'clj-lib.core/all-prefixs
clj-lib.core=>; prepare to join the country code and prefix-asn, create hash-map from IP2Location-LITE 

clj-lib.core=> (def cc-index (create-index test-file-sample))
#'clj-lib.core/cc-index
clj-lib.core=>; execute joining prefix-asn and the country code

clj-lib.core=> (def result (lookup-cc cc-index all-prefixs))
#'clj-lib.core/result
clj-lib.core=>; finally, write data as csv file
clj-lib.core=> (write-list-data result "prefix-cc-asn.csv")

```

from prefix-cc-asn.csv, tool/make-mmdb.sh generate a mmdb.

```
$ sh tool/make-mmdb.sh
```

### more challenges
- Make faster. The process both lookup prefixes and find country code is able to be threading.

