#!/bin/zsh
foreach FILE (testzas/*.zas)
echo $FILE
./zas $FILE
echo
end