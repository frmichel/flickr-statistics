# Flickr-statistics

This project provides java tools targeted to collect data from Flickr every day, and process them in order to investigate 2 subjects:
- (i) the number and rate of uploads to Flickr (see http://www.flickr.com/photos/franckmichel/6855169886/), and
- (ii) produce statistical reports about explored photos and compare them with other unexplored photos in order to figure out what makes explored photos special (see http://www.flickr.com/photos/franckmichel/8825511026/ for some tentative conclusions).

This 2nd point deals with number of views, comments, favorites, tags, groups, the time at which photos were posted (upload time), and some characteristics about photos' owners: 
number of photos they have, and number of members they follow.

Tools allow to report statistics either monthly or over a given period of time.

## REPORTS:

Reports fall into several categories.

- uploads: total number of photos uploaded every day, hour by hour
- activity about explored and non-explored photos:
  - distribution of number of photos in function of the number of times they were viewed
  - distribution of number of photos in function of the number of comments they have
  - distribution of number of photos in function of the number of favs they have
  - distribution of number of photos in function of the number of groups they are posted to
  - distribution of number of photos in function of the owner's number of photos
  - distribution of number of photos in function of the owner's number of contacts
  - distribution of number of photos in function of whether they have a geographical location not
  - distribution of number of photos in function of the time of day they were uploaded
  - average number of contacts and photos per user

In addition, reports about groups and tags vary whether they apply to one month or to an arbitrary time period:

Reports over a given time period:
- tag: list the tags of explored photos, sorted by number of hits.
- group: 
    - list the groups that explored photos are in, sorted by number of hits
    - compute the ratio: nb of explored photos in a group / total nb of photos uploaded to that group
      during the same time slot.

Monthly reports:
- tag: average and max number of tags of explored photo
- group: average and max number of groups that explored photos belong to


### "Probability" of a photo to be explored as a function of the week day and hour

Class ProcessProbabilityPerWeekDayAndHour computes the ratio of the number of explored photos / number of photos uploaded, as function of the post time (0 to 23h) AND the week day.

This mashes up data from the uploads and the activity (post date and time).


## HOW TO USE

### Data collection

Folder 'scripts' provides several shell scripts that I use to run the data collection every day.

### Run reports

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
    fm.flickr.stat.action.uploads = on  
    fm.flickr.stat.action.activity = off  
    fm.flickr.stat.action.anyphoto = off  

Lastly, run one of the main classes:
- data collection process (fm.flickr.stat.CollectPhotosData),
- monthly reports (fm.flickr.stat.ProcessMonthlyStats),
- reports over an arbitrary time period (fm.flickr.stat.ProcessStats),
- "Probability" of a photo to be explored as a function of the week day and hour (fm.flickr.stat.ProcessProbabilityPerWeekDayAndHour)

