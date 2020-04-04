# clj-lib.snmp

- uses [snmp4j](https://www.snmp4j.org/)

## functions

- [(clj-lib.snmp.core) snmpget-v2c](#snmpget-v2c)
- [(clj-lib.snmp.core) run-snmp-get](#run-snmp-bulkwalk)
- [(clj-lib.snmp.core) run-snmp-bulkwalk](#run-snmp-bulkwalk)

## snmpget-v2c

call snmp-get(v2c).

args:

- host-list string
- community string
- oid-list  string[] 

example:

```
clj-lib.core=> (pprint (snmpget-v2c "192.168.0.100" "public" '("1.3.6.1.2.1.1.3.0")))
({"1.3.6.1.2.1.1.3.0" "23 days, 19:59:14.56"})
nil
```

## snmpbulkwalk-v2c

call snmp-bulkwalk(v2c).

args:

- host-list string
- community string
- oid       string

example:

```
clj-lib.core=> (pprint (snmpbulkwalk-v2c "192.168.0.100" "public" "1.3.6.1.2.1.31.1.1.1.1"))
{"1.3.6.1.2.1.31.1.1.1.1.1" "lo",
 "1.3.6.1.2.1.31.1.1.1.1.10" "br-ex",
 "1.3.6.1.2.1.31.1.1.1.1.17" "vxlan_sys_4789",
 "1.3.6.1.2.1.31.1.1.1.1.3" "enp2s0",
 "1.3.6.1.2.1.31.1.1.1.1.11" "br-tun",
 "1.3.6.1.2.1.31.1.1.1.1.5" "enp4s0",
 "1.3.6.1.2.1.31.1.1.1.1.7" "virbr0-nic",
 "1.3.6.1.2.1.31.1.1.1.1.8" "ovs-system",
 "1.3.6.1.2.1.31.1.1.1.1.2" "enp1s0",
 "1.3.6.1.2.1.31.1.1.1.1.9" "br-int",
 "1.3.6.1.2.1.31.1.1.1.1.6" "virbr0",
 "1.3.6.1.2.1.31.1.1.1.1.4" "enp3s0"}
nil
clj-lib.core=>
```

##run-snmp-get

call snmp-get(v2c) in each host parallelly.
and get the result synchronously.

args:

- host-list string[]
- community string
- oid-list  string[] 

example:

```
clj-lib.core=> (pprint (run-snmp-get '("192.168.0.100", "192.168.0.102") "public" '("1.3.6.1.2.1.1.3.0" "1.3.6.1.2.1.1.4.0" )))
{"192.168.0.100"
 ({"1.3.6.1.2.1.1.3.0" "23 days, 19:51:48.04"}
  {"1.3.6.1.2.1.1.4.0" "Me <me@example.org>"}),
 "192.168.0.102"
 ({"1.3.6.1.2.1.1.3.0" "24 days, 23:27:53.95"}
  {"1.3.6.1.2.1.1.4.0" "Me <me@example.org>"})}
nil
clj-lib.core=>
```

##run-snmp-bulkwalk

call snmp-bulkwalk(v2c) in each host parallelly.
and get the result synchronously.

args:

- host-list string[]
- community string
- oid       string 

example:

```
clj-lib.core=> (pprint (run-snmp-bulkwalk '("192.168.0.100" "192.168.0.102") "public" "1.3.6.1.2.1.31.1.1.1.1"))
{"192.168.0.100"
 {"1.3.6.1.2.1.31.1.1.1.1.1" "lo",
  "1.3.6.1.2.1.31.1.1.1.1.10" "br-ex",
  "1.3.6.1.2.1.31.1.1.1.1.17" "vxlan_sys_4789",
  "1.3.6.1.2.1.31.1.1.1.1.3" "enp2s0",
  "1.3.6.1.2.1.31.1.1.1.1.11" "br-tun",
  "1.3.6.1.2.1.31.1.1.1.1.5" "enp4s0",
  "1.3.6.1.2.1.31.1.1.1.1.7" "virbr0-nic",
  "1.3.6.1.2.1.31.1.1.1.1.8" "ovs-system",
  "1.3.6.1.2.1.31.1.1.1.1.2" "enp1s0",
  "1.3.6.1.2.1.31.1.1.1.1.9" "br-int",
  "1.3.6.1.2.1.31.1.1.1.1.6" "virbr0",
  "1.3.6.1.2.1.31.1.1.1.1.4" "enp3s0"},
 "192.168.0.102"
 {"1.3.6.1.2.1.31.1.1.1.1.1" "lo",
  "1.3.6.1.2.1.31.1.1.1.1.10" "br-ex",
  "1.3.6.1.2.1.31.1.1.1.1.12" "vxlan_sys_4789",
  "1.3.6.1.2.1.31.1.1.1.1.3" "enp2s0",
  "1.3.6.1.2.1.31.1.1.1.1.11" "br-tun",
  "1.3.6.1.2.1.31.1.1.1.1.5" "enp4s0",
  "1.3.6.1.2.1.31.1.1.1.1.7" "virbr0-nic",
  "1.3.6.1.2.1.31.1.1.1.1.8" "ovs-system",
  "1.3.6.1.2.1.31.1.1.1.1.2" "enp1s0",
  "1.3.6.1.2.1.31.1.1.1.1.9" "br-int",
  "1.3.6.1.2.1.31.1.1.1.1.6" "virbr0",
  "1.3.6.1.2.1.31.1.1.1.1.4" "enp3s0"}}
nil
clj-lib.core=>
```
