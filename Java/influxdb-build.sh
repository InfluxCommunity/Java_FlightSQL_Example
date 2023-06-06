docker build \
  --build-arg SOURCE_HOST=$INFLUX_SRC_CLUSTER_URL \
  --build-arg READ_TOKEN=$INFLUX_READ_TOKEN \
  --build-arg SOURCE_DATABASE_NAME=$INFLUX_SRC_DB_NAME \
  --build-arg DOWNSAMPLE_QUERY="$QUERY" \
  --build-arg TARGET_CLUSTER_URL=$INFLUX_TGT_CLUSTER_URL \
  --build-arg WRITE_TOKEN=$INFLUX_WRITE_TOKEN \
  --build-arg TARGET_DATABASE_NAME=$INFLUX_TGT_DB_NAME \
  --build-arg TARGET_TABLE_NAME="$INFLUX_TGT_TBL_NAME" \
  -t javaflight .