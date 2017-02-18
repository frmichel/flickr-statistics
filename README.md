# Flickr-statistics

This project provides java tools targeted to collect data from Flickr in order to explore 2 subjects: (i) the number and rate of uploads to Flickr (see http://www.flickr.com/photos/franckmichel/6855169886/), and (ii) produce statistical reports about explored photos and compare them with other unexplored photos in order to figure out what makes explored photos so special (see http://www.flickr.com/photos/franckmichel/8825511026/ for my conclusions). This 2nd point deals with number of views, comments, favorites, tags, groups, as well as photo owners and the time they were posted.

Tools allow to report statistics either daily or monthly.

## REPORTS:


### Daily reports

- activity about explored and non-explored photos:
  - distribution of number of photos in function of the number of groups they are posted to
  - distribution of number of photos in function of the number of times they were viewed
  - distribution of number of photos in function of the number of comments they have
  - distribution of number of photos in function of the number of favs they have
  - distribution of number of photos in function of the owner's number of photos
  - distribution of number of photos in function of the owner's number of contacts
- tag: list the tags of explored photos, sorted by number of hits
- time: distribution of explored photos in function of the hour of day when they were uploaded
- group: 
    - list the groups that explored photos are in, sorted by number of hits
    - compute the ratio of "nb of explored photos in a group / total nb of photos uploaded to that group"
      during the same time slot.
- uploads: total number of photos uploaded every day


### Monthly reports

- tag: average and max number of tags per explored photo for each month
- user: for all users which have explored photos during each month, show the average and max number of contacts 
  and photos that they have, per month
- time: distribution of explored photos in function of the hour of day when they were uploaded, per month
- group: average and max number of groups that explored photos belong to, per month
- activity about explored and non-explored photos: same as daily reports, broken down by month.
- uploads: total number of photos uploaded every month


### Probability of a photo to be explored as a function of the week day and hour

Ratio of the number of explored photos / number of photos uploaded, as function of the post time (0 to 23h) AND the week day. 


## HOW TO USE

To use this set of tools you should be familiar with java development, and have a Flicr API key of your own.

Sorry I didn't have time to work on a GUI. But for a developer the code is rather self-explanatory, and I've tried to write quite a lot of comments.

You have to import this project into your favorite IDE (Eclipse or NetBeans for instance).
Then:
- edit file resource/faw.properties, and set the parameters with your own values:  
  fm.flickr.api.wrapper.flickr_apikey = your_api_key  
  fm.flickr.api.wrapper.flickr_secret = your_key_secret
- edit file resource/config.properties and set properties:
  - start and end dates are used by both the data collection process (fm.flickr.stat.CollectPhotosData.java) 
    and the statistical reports computation fm.flickr.stat.Process*.java):  
    fm.flickr.stat.startdate = 2013-04-01  
    fm.flickr.stat.enddate = 2013-07-31
  - set features on or off, again this applies to both the data collection or later on during reports computation:  
    fm.flickr.stat.action.group = off  
    fm.flickr.stat.action.tag = off  
    fm.flickr.stat.action.time = off  
    fm.flickr.stat.action.user = off  
    fm.flickr.stat.action.uploads = on  
    fm.flickr.stat.action.activity = off  
    fm.flickr.stat.action.anyphoto = off  

At last, run the main classes: data collection process (fm.flickr.stat.CollectPhotosData.java), and reports computation (fm.flickr.stat.Process*.java).
