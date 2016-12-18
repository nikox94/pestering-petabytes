#!/usr/bin/env python2

queryLimitPerMonth = 1000000
qu_per_sec = queryLimitPerMonth/float(31*24*60*60)
sec_per_query = 1/qu_per_sec
print "query per second = " + str(qu_per_sec)
print "sec per query = " + str(sec_per_query)
