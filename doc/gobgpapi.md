### example of list-path for ipv4-unicast
```
clj-lib.core=> (use 'clj-lib.gobgpapi.core)
nil
clj-lib.core=> (use 'clj-lib.gobgpapi.listpath)
nil
clj-lib.core=> (def gcli (gobgp-client "localhost:50051"))
#'clj-lib.core/gcli
clj-lib.core=> (pprint (list-path gcli :global 4 :unicast "103.105.48.64/30"))
[{:prefix "103.105.48.0/24",
  :paths
  ({:path
    {:Origin 0,
     :AsPath ([64515 65534 20473 2914 62240 136620]),
     :NextHop "169.254.169.254",
     :LocalPref 100,
     :Communities ("20473:24" "20473:100" "20473:2914" "64515:44"),
     :LargeCommunities ("20473:0:3009036124" "20473:100:2914")},
    :best true,
    :age 1585969129,
    :source_asn 64939}
   {:path
    {:Origin 0,
     :AsPath ([64515 65534 20473 2914 62240 136620]),
     :NextHop "169.254.169.254",
     :LocalPref 100,
     :Communities ("20473:100" "20473:2914" "64515:44"),
     :LargeCommunities ("20473:100:2914")},
    :best false,
    :age 1585969166,                                                                       [1/569]
    :source_asn 64939})}
 {:prefix "0.0.0.0/0",
  :paths
  ({:path
    {:Origin 0,
     :AsPath ([64515]),
     :NextHop "169.254.169.254",
     :LocalPref 100},
    :best true,
    :age 1585969120,
    :source_asn 64939}
   {:path
    {:Origin 0,
     :AsPath ([64515]),
     :NextHop "169.254.169.254",
     :LocalPref 100},
    :best false,
    :age 1585969163,
    :source_asn 64939})}]
nil
clj-lib.core=>
```

### ex add-path/delete-path
```
clj-lib.core=> (use 'clj-lib.gobgpapi.core)
nil
clj-lib.core=> (use 'clj-lib.gobgpapi.listpath)
nil
clj-lib.core=> (use 'clj-lib.gobgpapi.unicast)
nil
clj-lib.core=> (def example-call-args
          #_=>   { :origin     :egp
          #_=>     :nexthop    "2001:db8:cafe::1"
          #_=>     :ipver      6
          #_=>     :prefixes   {:prefix "2001:db8:cafe::" :prefixlen 64}
          #_=>     :communities [ 0xffff029a "65432:999" ]
          #_=>     :asn  65001
          #_=>     :med 100
          #_=>     :localpref 200
          #_=>   })
clj-lib.core=> (def gcli (gobgp-client "localhost:50051"))
#'clj-lib.core/gcli
clj-lib.core=> (add-path example-call-args gcli)
#object[gobgpapi.Gobgp$AddPathResponse 0xbf8dfb7 "uuid: \"\\206gFI\\324\\210NK\\260-\\037\\353\\274wYZ\"\n"]
clj-lib.core=> (pprint (list-path gcli :global 6 :unicast "2001:db8:cafe:/64"))
[{:prefix "2001:db8:cafe::/64",
  :paths
  ({:path
    {:Origin 1,
     :Communities (":BLACKHOLE" "65432:999"),
     :MED 100,
     :LocalPref 200,
     :MpReachNLRI
     {:family {:afi "AFI_IP6", :safi "SAFI_UNICAST"},
      :next_hops ["2001:db8:cafe::1"],
      :nlris
      {:IPAddresPrefix {:prefix "2001:db8:cafe::", :prefixlen 64}}}},
    :best true,
    :age 1585970825,
    :source_asn 65001})}]
nil
clj-lib.core=>
```
