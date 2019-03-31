#!/usr/bin/expect
while 1 {
expect
spawn sudo bluetoothctl
sleep 1
send "power on\r"
send "discoverable on\r"
send "pairable on\r"
send "agent NoInputNoOutput\r"
set timeout 1
expect "Agent registered"
    send "default-agent\r"
set timeout 20
expect "Request authorization"
    send "yes\r"
sleep 1
send "discoverable off\r"
sleep 1
send "exit\r"
}
