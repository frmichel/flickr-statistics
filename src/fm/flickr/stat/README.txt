ProcessDailyStats:
==================

Read data between fm.flickr.stat.startdate and fm.flickr.stat.enddate, and computes statistics for categories group, tag, user, time, user, 
upload. The results are displyaed on the standard output:
- tag: list the tags of explored photos, sorted by number of hits
- user: for all users which have explored photos, compute the number of contacts and photos that they have:
  average, std deviation, and max values.
- time: 
    - distribution of explored photos in function of the hour of day when they were uploaded (number of photos explored that were uploaded at midnight, 1am, 2am, etc.)
    - time to explore, ie. average and max time elapsed between the moment the photo was posted and the moment it appears in the Explorer
- group: 
    - list the groups that explored photos are in, sorted by number of hits
    - Optionally (fm.flickr.stat.group.proba), file groups/group_explore_proba_<startdate<_<enddate>.csv: compute the ratio of 
      explored photos / photos uploaded to a group during the same time slot.
      This takes a long time (lots of requests), to be activated cautiously...
- upload: distribution of number of photos posted to Flickr in function of the hour of day

ProcessMonthlyStats:
====================

Read all available data from the month of fm.flickr.stat.startdate to the month of fm.flickr.stat.enddate, and computes results consilated by month, 
for categories group, tag, user, time, user, activity and upload:
- tag: file tags/montly_results.csv: average and max number of tags per explored photo for each month
- user: file users/montly_results.csv: for all users which have explored photos during each month, compute the average and max number 
  of contacts and photos that they have
- time: 
    - file times/monthly_times_t2e.csv: average and max time elapsed between the moment the photo was posted and the moment it appears in the Explorer
    - file times/monthly_times_distrib.csv: distribution of explored photos in function of the hour of day when they were uploaded
- group: file groups/montly_results.csv: compute the average and max number of groups that explored photos belong to
- upload: file uploads/montly_results.csv: distribution of number of photos posted to Flickr in function of the hour of day, broken down by month
  Tab "yearly" tab of the Excel sheet uploads/montly_results.xls provides a view of only the last column 'total'.
  
  The results from uploads/montly_results.csv should also be copied into file times/monthly_times_distrib.xls, in tab 'uploaded' 
  that allows to compute the ratio of number of explored photos / number of photos uploaded by hour of day, broken down by month 
  
ProcessProbabilityPerWeekDayAndHour:
====================================

Read data between fm.flickr.stat.startdate and fm.flickr.stat.enddate, and compute the ratio of the number of 
explored photos / number of photos posted, as function of the post time (0 to 23h) AND the week day. 
Results are displayed on the std output, and manually inserted into file proba_explo_per_hour_week.csv.

ProcessUploadsPerDay:
==========================

This specific main class simply reports what already exists in daily files, namely the total number of photos
uploaded every day (the last column of daily data files), and stores the result into file uploads/daily_uploads.csv.
