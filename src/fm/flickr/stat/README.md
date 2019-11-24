# ComputeStatsTimeframe and ComputeStatsMonthly

ComputeStatsTimeframe processes data between `fm.flickr.stat.startdate` and `fm.flickr.stat.enddate`.
ComputeStatsMonthly reads all available data from the month of `fm.flickr.stat.startdate` to the month of `fm.flickr.stat.enddate`, and computes results broken down by month.

Both tools compute statistics for categories group, tag, activity and upload, stored in the following files:

- tag:
    - file `tags/result_sorted_listed.txt`: list the tags of explored photos, sorted by number of hits
    - file `tags/result_avg.csv`: average and max number of tags per explored photo for each month
- group: 
    - file `groups/result_sorted_listed.txt`: list the groups of explored photos, sorted by number of hits
    - file `groups/result_avg.csv`: average and max number of groups that explored photos belong to
- upload: file `uploads/distrib_results.csv`: distribution of number of explored photos posted to Flickr as a function of the hour of day
- activity and anyphoto:
    - file `distrib_view.csv`: distribution of number of explored photos as a function of the number of times they were viewed
    - file `distrib_fav.csv`: distribution of number of explored photos as a function of the number of favs they have
    - file `distrib_comment.csv`: distribution of number of explored photos as a function of the number of comments they have
    - file `distrib_group.csv`: distribution of number of explored photos as a function of the number of groups they are posted to
    - file `distrib_tag.csv`: distribution of number of explored photos as a function of the number of tags that they have
    - file `distrib_time.csv`: distribution of explored photos as a function of the hour of day they were uploaded
    - file `distrib_location.csv`: distribution of explored photos in function of whether they have a geographical location
    - file `distrib_owners_photo.csv` shows the distribution of users per number of photos that they have (starting 15/11/2012)
    - file `distrib_owners_contact.csv` shows the distribution of users per number of contacts that they have (starting 15/11/2012)
    - file `user_average.csv`: average number of contacts that explored photos' owners have (starting 15/11/2012)
    
# ProcessProbabilityPerWeekDayAndHour

Read data between `fm.flickr.stat.startdate` and `fm.flickr.stat.enddate`, and compute the ratio of the number of 
explored photos / number of photos posted, broken down by post time (0 to 23h) AND week day.

Results are stored in file csv file `proba_explo.csv`.

# ProcessUploadsPerDay

This specific main class simply reports what already exists in daily files, namely the total number of photos
uploaded every day (the last column of daily data files), and stores the result into file uploads/daily_uploads.csv.
It is useful to show the weekly cycles.
