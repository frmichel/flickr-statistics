#!/bin/bash
# This script runs the retrieval of activity data about non explored photos, posted ONE MONTH AGO (30 days ago more precisely),
# using a list of photo ids collected by script collect-recently-posted.sh and stored in
# /home/fmichel/franck/FlickrStatistics/stats/anyphoto/ids.

TODAY=`date --date="29 days ago" "+%Y-%m-%d"`
YESTERDAY=`date --date="30 days ago" "+%Y-%m-%d"`
echo "Start date: $TODAY"
echo "End date: $YESTERDAY"

PHOTOIDS=/home/fmichel/franck/FlickrStatistics/stats/anyphoto/ids/${YESTERDAY}.txt
if [ ! -f $PHOTOIDS ]
then
    echo "WARNING: File $PHOTOIDS does not exist."
    exit 0
fi

cd /home/fmichel/franck/FlickrStatistics

/usr/bin/java -cp "bin:lib/commons-beanutils-1.8.3.jar:lib/commons-collections-3.2.1.jar:lib/commons-configuration-1.7.jar:lib/commons-lang-2.6.jar:lib/commons-logging-1.1.1.jar:lib/log4j-1.2.16.jar" \
-Dfm.flickr.stat.startdate=$YESTERDAY \
-Dfm.flickr.stat.enddate=$TODAY \
-Dfm.flickr.api.wrapper.flickr_apikey=your_api_key \
-Dfm.flickr.api.wrapper.flickr_secret=your_secret \
-Dfm.flickr.stat.action.group=off \
-Dfm.flickr.stat.group.proba=off \
-Dfm.flickr.stat.action.tag=off \
-Dfm.flickr.stat.action.activity=off \
-Dfm.flickr.stat.action.uploads=off \
-Dfm.flickr.stat.action.anyphoto=on \
-Dfm.flickr.stat.photoslist=$PHOTOIDS \
-Dfm.flickr.stat.anyphoto.dir="stats/anyphoto-1monthago/" \
fm.flickr.stat.CollectPhotosData
