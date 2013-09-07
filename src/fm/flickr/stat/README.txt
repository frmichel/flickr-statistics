ProcessDailyStats:
==================

Read data between fm.flickr.stat.startdate and fm.flickr.stat.enddate, and computes statistics for categories group, tag, user, time, activity and upload. The results are displayed on the standard output:
- tag: list the tags of explored photos, sorted by number of hits
- user: for all users which have explored photos, compute the number of contacts and photos that they have:
  average, std deviation, and max values.
- time: distribution of explored photos in function of the hour of day when they were uploaded (number of photos explored that were 
  uploaded at midnight, 1am, 2am, etc.)
- group: 
    - list the groups that explored photos are in, sorted by number of hits
    - Optionally (fm.flickr.stat.group.proba), file groups/group_explore_proba_<startdate>_<enddate>.csv: compute the ratio of 
      "nb of explored photos in a group / total nb of photos uploaded to that group" during the same time slot.
      This takes a long time (lots of requests), to be activated cautiously...
- upload: distribution of number of photos posted to Flickr in function of the hour of day
- activity about explored photos:
	- distribution of number of explored photos in function of the number of groups they are posted to
	- distribution of number of explored photos in function of the number of times they were viewed
	- distribution of number of explored photos in function of the number of comments they have
	- distribution of number of explored photos in function of the number of favs they have
	- distribution of number of explored photos in function of the owner's number of photos (data collected starting 15 Nov 2012)
	- distribution of number of explored photos in function of the owner's number of contacts (data collected starting 15 Nov 2012)
- activity about any unexplored photos: data collected starting 8 Jan. 2013 => for photos starting at 8 Dec. 2012:
	- distribution of number of explored photos in function of the number of groups they are posted to
	- distribution of number of explored photos in function of the number of times they were viewed
	- distribution of number of explored photos in function of the number of comments they have
	- distribution of number of explored photos in function of the number of favs they have
	- distribution of number of explored photos in function of the owner's number of photos
	- distribution of number of explored photos in function of the owner's number of contacts


ProcessMonthlyStats:
====================

Read all available data from the month of fm.flickr.stat.startdate to the month of fm.flickr.stat.enddate, and computes results consilated 
by month, for categories group, tag, user, time, user, activity and upload:
- tag: file tags/montly_results.csv: average and max number of tags per explored photo for each month
- user: for all users which have explored photos during each month, file users/monthly_user_average.csv shows the average and max number of contacts and photos that they have
- time: file times/monthly_times_distrib.csv: distribution of explored photos in function of the hour of day when they were uploaded
- group: file groups/montly_results.csv: compute the average and max number of groups that explored photos belong to
- upload: file uploads/montly_results.csv: distribution of number of photos posted to Flickr in function of the hour of day, broken down by month
  Tab "yearly" of the Excel sheet uploads/montly_results.xls provides a view of only the last column 'total'.
  The results from uploads/montly_results.csv should also be copied into file times/monthly_times_distrib.xls, in tab 'uploaded' 
  that allows to compute the ratio of number of explored photos / number of photos uploaded by hour of day, broken down by month.
- activity and anyphoto:
	- file monthly_distrib_group.csv: distribution of number of explored photos in function of the number of groups they are posted to
	- file monthly_distrib_view.csv: distribution of number of explored photos in function of the number of times they were viewed
	- file monthly_distrib_comment.csv: distribution of number of explored photos in function of the number of comments they have
	- file monthly_distrib_fav.csv: distribution of number of explored photos in function of the number of favs they have
	- file monthly_distrib_owners_photo.csv shows the distribution of users per number of photos that they have (starting 15/11/2012)
	- file monthly_distrib_owners_contact.csv shows the distribution of users per number of contacts that they have (starting 15/11/2012)

  
ProcessProbabilityPerWeekDayAndHour:
====================================

Read data between fm.flickr.stat.startdate and fm.flickr.stat.enddate, and compute the ratio of the number of 
explored photos / number of photos posted, as function of the post time (0 to 23h) AND the week day. 
Results are displayed on the std output, and manually inserted into file proba_explo_per_hour_week.csv.

ProcessUploadsPerDay:
=====================

This specific main class simply reports what already exists in daily files, namely the total number of photos
uploaded every day (the last column of daily data files), and stores the result into file uploads/daily_uploads.csv.
It is useful to show the weekly cycles.
