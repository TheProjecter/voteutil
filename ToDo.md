# To Do? #

## General ##
  * Better STV support? More Proportional Representation methods?
  * Javascript? Is there a case for running the election in client side on the browser? Maybe a gadget/extension for Google Docs spreadsheets to count votes entered into a spreadsheet there.
  * More import capability? Try to parse general comma-separated-value input? (right now No, and rely on users to write a little custom perl or similar to munge their data into a format these tools understand.)
  * Cleanup. I have left some development and debugging cruft lying around in some of the code.
  * Testing. In progress, see below.

## Java ##
  * More and better javadoc. (always in progress!)
  * implement SummableVotingSystem in methods like ~~VRR~~, Histogram, Approval, ~~Raw~~, ...

## C++ ##
  * C++: Implement more dynamic name=value C++ election methods. (Until then, use C versions, but I aim to rewrite each of these methods in the native dialect of the various languages and not just be transliterations.)

# Done Lately #
  * Testing. Running C and Java implementations against each other on random vote data to check that they get the same answer. Some inconsistencies have been found and I won't consider this done until I can run 10000 random data sets with full agreement between all implementations. - 2008-05-30
  * Improved memory efficiency Java implementation of several methods that need to hold the whole ballot data set in memory. - 2008-06-13
  * Raw and VRR implement SummableVotingSystem. - 2008-10-28
  * Java reorganization. Many old deprecated methods had the prime namespace. They were moved to org.bolson.vote.staticballot.`*` and the Named`*` types were renamed without the "Named" prefix to make it more clear that they are the primary and preferred implementation. There is source for adapters in org/bolson/vote/NamedAdapters.tar.gz - 2009-02-18