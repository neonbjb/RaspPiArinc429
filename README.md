# RaspPiArinc429
This code is from a project I had to enable a Raspberry Pi to communicate with aircraft avionics via an ARINC 429 link. 

To enable this functionality, I requested a sample DEI1016 and MCP23017 from Device Electronics. I offered to buy these chips 
from them, but they generally do not sell in small quantities.

I used this code to successfully communicate with ARINC 429 devices, but I did not document my wiring set-up. I think most of
the code is pretty self-explanatory in this regard, though. If this is something you are attempting, just take a look at the
pin map structures in *Driver.java code. Good luck!
