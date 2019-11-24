# Flickr-statistics

This project provides java tools targeted to collect data from Flickr every day, and process them in order to investigate 2 subjects:
- (i) the number and rate of uploads to Flickr (see http://www.flickr.com/photos/franckmichel/6855169886/), and
- (ii) produce statistical reports about explored photos and compare them with other unexplored photos in order to figure out what makes explored photos special (see http://www.flickr.com/photos/franckmichel/8825511026/ for some tentative conclusions).

This 2nd point deals with number of views, comments, favorites, tags, groups, the time at which photos were posted (upload time), and some characteristics about photos' owners: 
number of photos they have, and number of members they follow.

Tools allow to report statistics either monthly or over a given period of time.

## REPORTS:

Reports fall into several categories.

- tag:
    - list the tags of explored photos, sorted by number of hits
    - average and max number of tags per explored photo for each month
- group: 
    - list the groups of explored photos, sorted by number of hits
    - average and max number of groups that explored photos belong to
- uploads: total number of photos uploaded every day, hour by hour
- activity about explored and non-explored photos:
  - distribution of number of photos in function of the number of times they were viewed
  - distribution of number of photos in function of the number of comments they have
  - distribution of number of photos in function of the number of favs they have
  - distribution of number of photos in function of the number of groups they are posted to
  - distribution of number of photos in function of the owner's number of photos
  - distribution of number of photos in function of the owner's number of contacts
  - distribution of number of photos in function of whether they have a geographical location
  - distribution of number of photos in function of the time of day they were uploaded

### "Probability" of a photo to be explored as a function of the week day and hour

Class ProcessProbabilityPerWeekDayAndHour computes the ratio of the number of explored photos / number of photos uploaded, as function of the post time (0 to 23h) AND the week day.

This mashes up data from the uploads and the activity (post date and time).


## HOW TO USE

### Data collection

Folder 'scripts' provides several shell scripts to run the data collection every day.

### Run reports

To use this set of tools you should be familiar with java development, and have a Flicr API key of your own.

You have to import this project into your favorite IDE (Eclipse or NetBeans for instance).
Then:
- edit file resource/faw.properties, and set the parameters with your own values:
```  
  fm.flickr.api.wrapper.flickr_apikey = your_api_key  
  fm.flickr.api.wrapper.flickr_secret = your_key_secret
```  
- edit file resource/config.properties and set properties:
  - start and end dates are used by both the data collection process (fm.flickr.stat.CollectPhotosData.java) 
    and the statistical reports computation fm.flickr.stat.Process*.java):  
```  
    fm.flickr.stat.startdate = 2013-04-01  
    fm.flickr.stat.enddate = 2013-07-31
```  
  - set features on or off, this applies to both the data collection and the computation of reports later on:  
```  
    fm.flickr.stat.action.group = on  
    fm.flickr.stat.action.tag = on 
    fm.flickr.stat.action.uploads = on
    fm.flickr.stat.action.activity = on  
    fm.flickr.stat.action.anyphoto = on
```  

Lastly, run one of the main classes:
- data collection process (fm.flickr.stat.CollectPhotosData),
- monthly reports (fm.flickr.stat.ComputeStatsMonthly),
- reports over an arbitrary time period (fm.flickr.stat.ComputeStatsTimeframe),
- "Probability" of a photo to be explored as a function of the week day and hour (fm.flickr.stat.ComputeProbabilityPerWeekDayAndHour)

