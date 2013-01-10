#!/bin/bash

cd /home/fmichel/franck

NOW=`date "+%Y-%m-%d %H:%M:%S"`
YESTERDAY=`date --date=yesterday "+%Y-%m-%d"`
OUTPUT="/home/fmichel/public_html/_flickr/explorer-date-found.txt"


#--- 1. Check if this date 'yesterday' has already been found in previous runs
grep --silent --ignore-case "$YESTERDAY" $OUTPUT
if test $? -eq 0; then
   # Explorer of $YESTERDAY already found in a previous run, exiting.
   exit 0
fi


#--- 2. Otherwise, look for the explored photos using the Flickr API
APIRESP=_api_resp.xml
wget --tries=3 --waitretry=5 --output-document=$APIRESP "http://api.flickr.com/services/rest/?method=flickr.interestingness.getList&api_key=ae7ff43c5fbbccd09a1643adf3160cd2&date=${YESTERDAY}&format=rest" 2>null >> /tmp/fmichel/explored.log

STATUS=""
# If searched date is before yestereday, the status should be failed
grep 'stat="fail"' $OUTPUT
grep --silent --ignore-case 'stat="fail"' $APIRESP > /dev/null
if test $? -eq 0; then
    STATUS="failed"
fi

# If date is yestereday, but the explorer has not been published yet, there is 0 result
grep --silent --ignore-case 'total="0"' $APIRESP > /dev/null
if test $? -eq 0; then
    STATUS="${STATUS} empty"
fi

if test "$STATUS" != ""; then
    # echo "$NOW: Explorer of $YESTERDAY not published yet, exiting. Reason: $STATUS"
    rm -f $APIRESP
    exit 0
fi

# Put the date in file $OUTPUT not to do it again during next run
echo "$YESTERDAY" >> $OUTPUT

# Get the explored photos data
echo "$NOW: Explorer of $YESTERDAY has just been published, running the stats retrieval..."
/home/fmichel/franck/run-stats-explorer-all.sh 2>&1 >> /tmp/fmichel/explored.log

# Get data about any other photo not explored but posted during the same period
NOW=`date "+%Y-%m-%d %H:%M:%S"`
echo "$NOW: Retrieving data about non explored photos..."
/home/fmichel/franck/run-stats-anyphoto.sh 2>&1 >> /tmp/fmichel/anyphoto.log

# Get data about any other photo not explored, posted one month ago
NOW=`date --date="30 days ago" "+%Y-%m-%d %H:%M:%S"`
echo "$NOW: Retrieving data about non explored photos..."
/home/fmichel/franck/run-stats-anyphoto-1monthago.sh 2>&1 >> /tmp/fmichel/anyphoto.log

rm -f $APIRESP
