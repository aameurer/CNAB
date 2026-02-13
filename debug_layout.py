
line = "1040001300001T 460000004910850000000   14999000002922185812922185        060320260000000000702680010000002922185                  092040125594000181MARTINS ENGENHARIA LTDA                           000000000000000040301                     "

print(f"Line length: {len(line)}")

def print_field(name, start, end):
    # Java substring is start:end, Python slice is start:end
    val = line[start:end]
    print(f"{name} ({start}-{end}): '{val}'")

print("--- Current Code ---")
print_field("NomePagador", 146, 186)
print_field("NumContrato", 186, 196)
print_field("ValorTarifa", 196, 211)
print_field("MotivoOcorrencia", 211, 221)

print("\n--- Hypothesis ---")
print_field("NomePagador (Shifted)", 148, 188)
print_field("NumContrato (Shifted)", 188, 198)
print_field("ValorTarifa (Shifted)", 198, 213)
print_field("MotivoOcorrencia (Shifted)", 213, 223)

print("\n--- Checking Tail ---")
print_field("Tail 213-end", 213, 240)
