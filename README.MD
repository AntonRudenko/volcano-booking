# Volcano trip ("some likes it hot" is internal of the project)
 
Application for booking a campsite on the small unique volcanic island.
You can book single camp site on an island.
Restrictions:
- maximum for 3 days
- 1 day in advance
- booking further than 1 month is not allowed
- 1 booking per user


## How to launch
### Requirements:
- maven
- java 16+
### Running
make executable and launch run.sh script (applicable for linux and MAC users, I don't use Windows :)


## Design notes
1.Since it is not regular booking system and have restrictions I decided to use them for my advantage. 
I understand that if I will need to add some more generic booking features it would require more code changes.
But some restrictions can really simplify code, so I would go with them.
Making application more general than it is needed can expand developing time a lot.
2.For booking record I decided to go with approach: each day of booking is one record in the table.
What advantage it gives to me:
- I can add index on the date of booking. So for data integrity (not booking twice one day) I will rely on the DB.
 From my experience the best way to be sure that your data in DB is good is to use indexes, constraints etc. DB is excellent at that.
 If something can go wrong it will. And if you control consistency only in the code, one day it could go wrong,
 and it will lead to very annoying bugs.
- Disadvantage is that we could have trice more data if we were adding 1 DB record for 1 booking with start and end date.
But since there is only one spot maximum we can have is 365 records in a year. This number is nothing for DB.
- Also, this model gives a good hint how to do with availability call. In such model this call is simple as querying 
 all records filtered by date. If there is a record then date is not available.
- To bound records together I will use generate booking UUID. This UUID will be also used to bound guest to the booking
to make DB normalized
3. For booking reference I use UUID. Since it is not serial it is quite safe to use it. If I would like more security
 I would use some hashing function for that.
4. Since we are expecting high concurrency on the get availability API, I will use cache on that method. 
This cache will just do parameter to result resolving. It can be more complex and store in memory days that are booked.
But I don't think we will benefit a lot from that in this application
5. When we will have error I will return special error object with code and message. It is good to have http codes,
but sometimes it is not enough.
6. I will sometimes rely on runtime exceptions and error handler. 
7. I'm using Spring data jpa, today in new projects we move to spring data jdbc, but this is simple app so I used JPA.
But I don't use @OneToOne annotation here. On purpose, I'm avoiding using such annotations. I control links myself.
Yes there is more boilerplate code, but I had a lot of problems with that in the past (strange errors, difficult optimisation,
 a lot of additional annotations) so I prefer to control saving entity graph myself.
8. I avoid using quite popular lombok. In my experience it long run it gives more problems than benefits. 
And it drives simple editors crazy. If somebody wants auto generated getters and setters etc. they should look at Kotlin for example.
9. Since I settle on in memory DB, I will use spring boot to init DB. But if it was using standalone DB, I would use
Liquibase
10. In real application I wouldn't use H2 as DB, because it has some strange behaviour sometimes, but it simplifies
development and deployment
11. Logs for simplicity are written into standard output
12. Most all of my tests are integration. I prefer to start with integration tests (if it is possible),
because if they are working it means that application is at least operational with common scenarios.
Since I have decent coverage I stopped there, but if it was real application I would prefer to have additional
unit tests

## API
Here are some examples how to use api. For more detailed info swagger links can be used.

Date format is: "YYYY-MM-DD"
### Check availability
`GET /booking?startDate=2022-05-21&endDate=2022-05-23`
#### Request
No request body here since it is get request
#### Response
Returns map of availability for each day, if value for date is false then date is unavailable
```json
{
    "availability": {
        "2021-09-27": true,
        "2021-09-28": false,
        "2021-09-29": true
    }
}
```

### Book dates
`POST /booking`
#### Request
```json
{
	"startDate": "2021-09-27",
	"endDate": "2021-09-29",
	"name": "Anton",
	"email": "anton@mail.com"
}
```
#### Response
```json
{
    "bookingId": "bd2b1863-d95f-4eaf-8e40-105f5af1487e"
}
```

### Update booking
Booking id is set in url

`PUT /booking/bd2b1863-d95f-4eaf-8e40-105f5af1487e`
#### Request
For update of specific field (for example name), just leave all others empty. To change date you must submit both 
start date and end date
```json
{
	"startDate": "2021-09-27",
	"endDate": "2021-09-29",
	"name": "Anton",
	"email": "anton@mail.com"
}
```
#### Response
empty response

### Cancel booking
Booking id is set in url

`DELETE /booking/bd2b1863-d95f-4eaf-8e40-105f5af1487e`
#### Request
empty request
#### Response
empty response

## Swagger links
1. Api docs: http://localhost:8080/v2/api-docs
2. UI http://localhost:8080/swagger-ui/