# clj-lib.mux-ssh

## abstraction
 The functions in clj-lib.mux-ssh provide remote invokation in os command level by multiplexing ssh. To use sessions in multiplexing, ssh-connection is established by external os command `/bin/ssh`.  Once getting connection, its socket and process stay in OS space. Invokation for user command uses this ssh process, this action is known as multiplexing ssh.


## functions

- [check-host](#check-host)
- [run-cmd](#run-cmd)
- [rum-cmd-th](#run-cmd-th)

## check-host

args:

- host string

return:
- true/false for available

## run-cmd

args:

- host string
- cmd  string[]
- function to process each line

## run-cmd-th

args is same to run-cmd 

returns (atom true)

To cancel the process, the flag returned would be inverse.
i.e. (swap! flag not)

## example

```
clj-lib.core=> (use 'clj-lib.mux-ssh.core)
nil
clj-lib.core=> (check-host "hnd.balus.xyz")
true
clj-lib.core=> (run-cmd "hnd.balus.xyz" '( "/bin/ping" "-c" "5" "8.8.8.8") println)
PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.
64 bytes from 8.8.8.8: icmp_seq=1 ttl=56 time=3.52 ms
64 bytes from 8.8.8.8: icmp_seq=2 ttl=56 time=1.02 ms
64 bytes from 8.8.8.8: icmp_seq=3 ttl=56 time=1.29 ms
64 bytes from 8.8.8.8: icmp_seq=4 ttl=56 time=1.10 ms
64 bytes from 8.8.8.8: icmp_seq=5 ttl=56 time=1.17 ms

--- 8.8.8.8 ping statistics ---
5 packets transmitted, 5 received, 0% packet loss, time 4016ms
rtt min/avg/max/mdev = 1.023/1.623/3.524/0.955 ms
0
clj-lib.core=> (def sig (run-cmd-th "hnd.balus.xyz" '( "/bin/ping" "-i" "3" "8.8.8.8") println))
#'clj-lib.core/sig
clj-lib.core=> PING 8.8.8.8 (8.8.8.8) 56(84) bytes of data.
64 bytes from 8.8.8.8: icmp_seq=1 ttl=56 time=4.15 ms
64 bytes from 8.8.8.8: icmp_seq=2 ttl=56 time=1.10 ms
64 bytes from 8.8.8.8: icmp_seq=3 ttl=56 time=1.25 ms
64 bytes from 8.8.8.8: icmp_seq=4 ttl=56 time=1.76 ms


clj-lib.core=> (swap! sig not)
false
clj-lib.core=>
```

