#!/bin/bash
gzip $1 
curl --insecure -F "image=@$1.gz"  https://rediclon.cedia.edu.ec/fs/UploadDownloadFileServlet
rm $1.gz