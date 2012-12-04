#!/bin/bash

#TODAY=`date --date=yesterday "+%Y-%m-%d"`
#YESTERDAY=`date --date="2 days ago" "+%Y-%m-%d"`

TODAY=`date "+%Y-%m-%d"`
YESTERDAY=`date --date=yesterday "+%Y-%m-%d"`

cd /home/fmichel/franck/FlickrStatistics

/usr/bin/java -cp "bin:lib/commons-beanutils-1.8.3.jar:lib/commons-collections-3.2.1.jar:lib/commons-configuration-1.7.jar:lib/commons-lang-2.6.jar:lib/commons-logging-1.1.1.jar:lib/log4j-1.2.16.jar" -Dfm.flickr.stat.startdate=$YESTERDAY -Dfm.flickr.stat.enddate=$TODAY -Dfm.flickr.api.wrapper.flickr_apikey=your_key_here -Dfm.flickr.api.wrapper.flickr_secret=your_secret_here -Dfm.flickr.stat.action.group=on -Dfm.flickr.stat.group.proba=off -Dfm.flickr.stat.action.tag=on -Dfm.flickr.stat.action.time=on -Dfm.flickr.stat.action.user=on -Dfm.flickr.stat.action.activity=on -Dfm.flickr.stat.action.uploads=on fm.flickr.stat.CollectInterestingnessData
