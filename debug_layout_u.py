
line_u = "1040001300002U 46000000000000000000000000000000000000000000000000000000000000000000000070268000000000070268000000000000000000000000000000100220261102202600000000000000000000000000000000000000000000000000000000000000000000000000000000       "

print(f"Line U length: {len(line_u)}")

def print_field_u(name, start, end):
    val = line_u[start:end]
    print(f"{name} ({start}-{end}): '{val}'")

print("--- Current Code Segment U ---")
print_field_u("Juros", 17, 32)
print_field_u("DataOcorrencia", 137, 145)
print_field_u("DataCredito", 145, 153)

# Search for the date
date_str = "10022026"
idx = line_u.find(date_str)
print(f"\nFound date '{date_str}' at index: {idx}")
