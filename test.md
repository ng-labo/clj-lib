*** clj-lib.gobgpapi.core-test

A gobgpd process is run before testing.

the part of configuration following,
```
[global.config]
  as = 65001
  router-id = ...
  local-address-list = ["0.0.0.0"]

[[neighbors]]
  [neighbors.config]
    neighbor-address = ...
    peer-as = 65001
    [neighbors.ebgp-multihop.config]
      enabled = true
  [[neighbors.afi-safis]]
    [neighbors.afi-safis.config]
      afi-safi-name = "ipv4-unicast"
  [[neighbors.afi-safis]]
    [neighbors.afi-safis.config]
      afi-safi-name = "ipv6-unicast"
  [[neighbors.afi-safis]]
    [neighbors.afi-safis.config]
      afi-safi-name = "ipv4-flowspec"
  [[neighbors.afi-safis]]
    [neighbors.afi-safis.config]
      afi-safi-name = "ipv6-flowspec"
```

*** clj-lib.snmp.core-test

A snmpd process is run before testing.

the part of configuration following,
```
view   systemonly  included   .1.3.6.1.2.1.31.1
```

