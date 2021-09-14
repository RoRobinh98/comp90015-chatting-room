# DS_Assignment1

Unsolved Issue:
1. 异常断连没有及时更新信息
2. If any room other than MainHall has an empty owner and becomes empty (i.e. has no contents) then the room is deleted immediately
3. createRoom返回的消息通过的是再RoomList message上多加了一条content
4. createRoom的join是可以用的，用户的join要调用两次join指令才成功

Checked and Passed Function on single client:
1. IdentityChange
2. createRoom
3. roomList - #list
