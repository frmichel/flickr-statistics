#!/bin/bash

TODAY=`date "+%Y-%m-%d"`
YESTERDAY=`date --date=yesterday "+%Y-%m-%d"`

cd /home/fmichel/franck/FlickrStatistics

/usr/bin/java -cp "bin:lib/commons-beanutils-1.8.3.jar:lib/commons-collections-3.2.1.jar:lib/commons-configuration-1.7.jar:lib/commons-lang-2.6.jar:lib/commons-logging-1.1.1.jar:lib/log4j-1.2.16.jar" -Dfm.flickr.stat.startdate=$YESTERDAY -Dfm.flickr.stat.enddate=$TODAY -Dfm.flickr.api.wrapper.flickr_apikey=ae7ff43c5fbbccd09a1643adf3160cd2 -Dfm.flickr.api.wrapper.flickr_secret=31c39525c70766a7 -Dfm.flickr.stat.action.group=on -Dfm.flickr.stat.action.tag=on -Dfm.flickr.stat.action.time=on -Dfm.flickr.stat.action.user=on -Dfm.flickr.stat.action.activity=off fm.flickr.stat.CollectData

