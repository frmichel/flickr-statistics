#!/bin/bash

TODAY=`date "+%Y-%m-%d"`
YESTERDAY=`date --date=yesterday "+%Y-%m-%d"`

TODAY=2012-02-16
YESTERDAY=2012-02-15

cd /home/fmichel/franck/FlickrStatistics

/usr/bin/java -cp "bin:lib/commons-beanutils-1.8.3.jar:lib/commons-collections-3.2.1.jar:lib/commons-configuration-1.7.jar:lib/commons-lang-2.6.jar:lib/commons-logging-1.1.1.jar:lib/log4j-1.2.16.jar" -Dfm.flickr.stat.photoslist=/home/fmichel/franck/photo-ids.txt -Dfm.flickr.stat.startdate=$YESTERDAY -Dfm.flickr.stat.enddate=$TODAY -Dfm.flickr.api.wrapper.flickr_apikey=ae7ff43c5fbbccd09a1643adf3160cd2 -Dfm.flickr.api.wrapper.flickr_secret=31c39525c70766a7 -Dfm.flickr.stat.action.group=off -Dfm.flickr.stat.action.tag=off -Dfm.flickr.stat.action.time=on -Dfm.flickr.stat.action.user=off -Dfm.flickr.stat.action.activity=off -Dfm.flickr.stat.time.dir="stats/times" fm.flickr.stat.CollectInterestingnessData

NOW=`date "+%Y-%m-%d-%H%M%S"`
mv stats/times/$YESTERDAY.log stats/times/$YESTERDAY.log.$NOW

