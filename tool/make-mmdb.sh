#!/bin/sh
TMPSCRIPT=/tmp/make-mmdb.$$
DATAPART=prefix-cc-asn.csv
OUTPUTFILE=ip-cc-asn.mmdb
cat > $TMPSCRIPT <<EOT
use MaxMind::DB::Writer::Tree;

my %types = (
    cc => 'utf8_string',
    asn => 'uint32',
);

my \$tree = MaxMind::DB::Writer::Tree->new(
    ip_version            => 6,
    record_size           => 32,
    database_type         => 'from IP2LocationLite',
    languages             => ['jp'],
    description           => {jp => '' },
    map_key_type_callback => sub { \$types{ \$_[0] } },
);
EOT

awk -F, '{ print "$tree->insert_network('\''" $1 "'\'' , { cc =>'\''" $2 "'\'' , asn => " $3 ", },);"}' $DATAPART >> $TMPSCRIPT

cat >> $TMPSCRIPT <<EOT

open my \$fh, '>:raw', '$OUTPUTFILE';
\$tree->write_tree(\$fh);
EOT
perl $TMPSCRIPT
ls -l $OUTPUTFILE
rm -f $TMPSCRIPT

