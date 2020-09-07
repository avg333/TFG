#!/bin/bash
for i in {0..19}
do
	x=$(( $RANDOM % 100 + 1 - 50))
	y=$(( $RANDOM % 100 + 1 - 50))
	java -jar UserEquipment.jar $i $x $y -d e 0.2 0 &
done

for i in {0..4}
do
	x=$(( $RANDOM % 100 + 1 - 50))
	y=$(( $RANDOM % 100 + 1 - 50))
	java -jar BaseStation.jar $i $x $y &
done