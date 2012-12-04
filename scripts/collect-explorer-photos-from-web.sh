#!/bin/bash
# This script collects the ids of explored photos from the Flickr web pages

cd /home/fmichel/franck

# This script runs at 1am to get the explored photos of the day before
#DATE=`date --date="2 days ago" "+%Y/%m/%d"`
DATE=`date --date=yesterday "+%Y/%m/%d"`

# The explorer html page contains 50 pages of 10 photos every day
NBPAGES=50
HTMLPAGE=_flickr_explorer.html

OUTPUTFILE=photo-ids.txt
TOKEN='<span class="photo_container pc_m"><a href="/photos/'

rm -f $OUTPUTFILE

page=1
while [ "$page" -le "$NBPAGES" ]
do
  echo "Getting page $page of explore on $DATE"
  wget --tries=3 --waitretry=5 --output-document=$HTMLPAGE "http://www.flickr.com/explore/interesting/${DATE}/page${page}"
  grep "$TOKEN" $HTMLPAGE | cut -d'/' -f4 >> $OUTPUTFILE

  page=`expr $page + 1`
  #sleep 5
done

rm $HTMLPAGE

