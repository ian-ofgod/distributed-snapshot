outfile_r=open('outfile_100', 'r')
filtered_r=open('filtered_outfile_100', 'w')

lines = outfile_r.readlines()

for line in lines:
    if line.startswith("APP"):
        filtered_r.write(line)

outfile_r.close()
filtered_r.close()