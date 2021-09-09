import re

outfile_r = open('outfile', 'r')
filtered_r = open('filtered_outfile.txt', 'w')

lines = outfile_r.readlines()

for line in lines:
    new_line = re.sub(r'(oilwells)(.*?)(-)', '-', line)
    new_line = re.sub(r'(\${main:0})', '0', new_line)
    filtered_r.write(new_line)

outfile_r.close()
filtered_r.close()
