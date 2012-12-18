#!/bin/bash
# This cript collects the ids of the last pictures posted to Flickr.
# It is intended to be run once an hour in order to retrieve enough ids a day (by default 150*24 = 3600 photos)
# 
# Later, files produced by this sctipt should be used to feed the Activity stat (instead of using ids from the explorer),
# this is run by script run-stats-anyphoto.sh.

cd /home/fmichel/franck/FlickrStatistics/stats/anyphoto/ids

# Date is that of zone PCST, where Flickr servers are, in order to collect ids during the
# period as the Explorer photos
DATE=`date --date="9 hours ago" "+%Y-%m-%d"`

OUTPUTXML=_flickr_recently_posted.xml
OUTPUTFILE=$DATE.txt
REQUEST='http://api.flickr.com/services/rest/?method=flickr.photos.getRecent&api_key=your_api_key&per_page=150&page=1&format=rest'

wget --tries=3 --waitretry=5 --output-document=$OUTPUTXML "$REQUEST" 2>&1

# Test if status is ailed
grep --silent --ignore-case 'stat="fail"' $OUTPUTXML > /dev/null
if test $? -eq 0; then
    echo "Error status failed:"
    grep --ignore-case 'err code=' $OUTPUTXML
    rm $OUTPUTXML
    exit 1
fi

grep '<photo id=\"' $OUTPUTXML | cut -d'"' -f2 >> $OUTPUTFILE
rm $OUTPUTXML
