#!/bin/bash

TODAY=`date "+%Y-%m-%d"`
YESTERDAY=`date --date=yesterday "+%Y-%m-%d"`

cd /home/fmichel/franck/FlickrStatistics

/usr/bin/java -cp "bin:lib/commons-beanutils-1.8.3.jar:lib/commons-collections-3.2.1.jar:lib/commons-configuration-1.7.jar:lib/commons-lang-2.6.jar:lib/commons-logging-1.1.1.jar:lib/log4j-1.2.16.jar" -Dfm.flickr.stat.startdate=$YESTERDAY -Dfm.flickr.stat.enddate=$TODAY -Dfm.flickr.stat.anyphoto.nbphotos=1000 -Dfm.flickr.api.wrapper.flickr_apikey=your_key_here -Dfm.flickr.api.wrapper.flickr_secret=your_secret_here fm.flickr.stat.CollectAnyPhotoData 

